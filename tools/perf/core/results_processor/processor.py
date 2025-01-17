# Copyright 2019 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""Implements the interface of the results_processor module.

Provides functions to parse command line arguments, process options, and the
entry point to start the processing of results.
"""

import argparse
import datetime
import json
import os
import re
import sys

from py_utils import cloud_storage
from core.results_processor import json3_output


TELEMETRY_RESULTS = '_telemetry_results.jsonl'
SUPPORTED_FORMATS = {
    'none': NotImplemented,
    'json-test-results': json3_output,
}


def ArgumentParser(standalone=False, legacy_formats=None):
  """Create an ArgumentParser defining options required by the processor."""
  all_output_formats = sorted(
      set(SUPPORTED_FORMATS).union(legacy_formats or ()))
  parser, group = _CreateTopLevelParser(standalone)
  group.add_argument(
      '--output-format', action='append', dest='output_formats',
      metavar='FORMAT', choices=all_output_formats, required=standalone,
      help=Sentences(
          'Output format to produce.',
          'May be used multiple times to produce multiple outputs.',
          'Avaliable formats: %s.' % ', '.join(all_output_formats),
          '' if standalone else 'Defaults to: html.'))
  group.add_argument(
      '--intermediate-dir', metavar='DIR_PATH', required=standalone,
      help=Sentences(
          'Path to a directory where intermediate results are stored.',
          '' if standalone else 'If not provided, the default is to create a '
          'new directory within "{output_dir}/artifacts/".'))
  group.add_argument(
      '--output-dir', default=_DefaultOutputDir(), metavar='DIR_PATH',
      help=Sentences(
          'Path to a directory where to write final results.',
          'Default: %(default)s.'))
  group.add_argument(
      '--reset-results', action='store_true',
      help=Sentences(
          'Overwrite any previous output files in the output directory.',
          'The default is to append to existing results.'))
  group.add_argument(
      '--results-label', metavar='LABEL',
      help='Label to identify the results generated by this run.')
  group.add_argument(
      '--upload-results', action='store_true',
      help='Upload generated artifacts to cloud storage.')
  group.add_argument(
      '--upload-bucket', default='output', metavar='BUCKET',
      help=Sentences(
          'Storage bucket to use for uploading artifacts.',
          'Supported values are: %s; or a valid cloud storage bucket name.'
          % ', '.join(sorted(cloud_storage.BUCKET_ALIASES)),
          'Defaults to: %(default)s.'))
  group.set_defaults(legacy_output_formats=[])
  return parser


def ProcessOptions(options):
  """Adjust result processing options as needed before running benchmarks.

  Note: The intended scope of this function is limited to only adjust options
  defined by the ArgumentParser above. One should not attempt to read or modify
  any other attributes that the options object may have.

  Currently the main job of this function is to tease out and separate output
  formats to be handled by the results processor, from those that should fall
  back to the legacy output formatters in Telemetry.

  Args:
    options: An options object with values parsed from the command line.
  """
  # The output_dir option is None or missing if the selected Telemetry command
  # does not involve output generation, e.g. "run_benchmark list", and the
  # argument parser defined above was not invoked.
  if getattr(options, 'output_dir', None) is None:
    return

  def resolve_dir(path):
    return os.path.realpath(os.path.expanduser(path))

  options.output_dir = resolve_dir(options.output_dir)

  if options.intermediate_dir:
    options.intermediate_dir = resolve_dir(options.intermediate_dir)
  else:
    if options.results_label:
      filesafe_label = re.sub(r'\W+', '_', options.results_label)
    else:
      filesafe_label = 'run'
    start_time = datetime.datetime.utcnow().strftime('%Y%m%dT%H%M%SZ')
    options.intermediate_dir = os.path.join(
        options.output_dir, 'artifacts', '%s_%s' % (filesafe_label, start_time))

  if options.upload_results:
    options.upload_bucket = cloud_storage.BUCKET_ALIASES.get(
        options.upload_bucket, options.upload_bucket)
  else:
    options.upload_bucket = None

  if options.output_formats:
    chosen_formats = sorted(set(options.output_formats))
  else:
    chosen_formats = ['html']

  options.output_formats = []
  for output_format in chosen_formats:
    if output_format == 'none':
      continue
    elif output_format in SUPPORTED_FORMATS:
      options.output_formats.append(output_format)
    else:
      options.legacy_output_formats.append(output_format)


def ProcessResults(options):
  """Process intermediate results and produce the requested outputs.

  This function takes the intermediate results generated by Telemetry after
  running benchmarks (including artifacts such as traces, etc.), and processes
  them as requested by the result processing options.

  Args:
    options: An options object with values parsed from the command line and
      after any adjustments from ProcessOptions were applied.
  """
  if not getattr(options, 'output_formats', None):
    return 0

  intermediate_results = _LoadIntermediateResults(
      os.path.join(options.intermediate_dir, TELEMETRY_RESULTS))

  for output_format in options.output_formats:
    if output_format not in SUPPORTED_FORMATS:
      raise NotImplementedError(output_format)

    formatter = SUPPORTED_FORMATS[output_format]
    formatter.Process(intermediate_results, options.output_dir)


def _CreateTopLevelParser(standalone):
  """Create top level parser, and group for result options."""
  if standalone:
    parser = argparse.ArgumentParser(
        description='Standalone command line interface to results_processor.')
    # In standalone mode, both the parser and group are the same thing.
    return parser, parser
  else:
    parser = argparse.ArgumentParser(add_help=False)
    group = parser.add_argument_group(title='Result processor options')
    return parser, group


def _LoadIntermediateResults(intermediate_file):
  """Load intermediate results from a file into a single dict."""
  results = {'benchmarkRun': {}, 'testResults': []}
  with open(intermediate_file) as f:
    for line in f:
      record = json.loads(line)
      if 'benchmarkRun' in record:
        results['benchmarkRun'].update(record['benchmarkRun'])
      if 'testResult' in record:
        results['testResults'].append(record['testResult'])
  return results


def _DefaultOutputDir():
  """Default output directory.

  Points to the directory of the benchmark runner script, if found, or the
  current working directory otherwise.
  """
  main_module = sys.modules['__main__']
  if hasattr(main_module, '__file__'):
    return os.path.realpath(os.path.dirname(main_module.__file__))
  else:
    return os.getcwd()


def Sentences(*args):
  return ' '.join(s for s in args if s)


def main(args=None):
  """Entry point for the standalone version of the results_processor script."""
  parser = ArgumentParser(standalone=True)
  options = parser.parse_args(args)
  ProcessOptions(options)
  return ProcessResults(options)

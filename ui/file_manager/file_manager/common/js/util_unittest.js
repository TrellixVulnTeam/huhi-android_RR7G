// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

let fileSystem;

function setUp() {
  // Override VolumeInfo.prototype.resolveDisplayRoot to be sync.
  VolumeInfoImpl.prototype.resolveDisplayRoot = function(successCallback) {
    this.displayRoot_ = this.fileSystem_.root;
    successCallback(this.displayRoot_);
    return Promise.resolve(this.displayRoot_);
  };

  fileSystem = new MockFileSystem('fake-volume');
  const filenames = [
    '/file_a.txt',
    '/file_b.txt',
    '/file_c.txt',
    '/file_d.txt',
    '/dir_a/file_e.txt',
    '/dir_a/file_f.txt',
    '/dir_a/dir_b/dir_c/file_g.txt',
  ];
  fileSystem.populate(filenames);

  window.loadTimeData.data = {
    SIZE_BYTES: '$1 bytes',
    SIZE_KB: '$1 KB',
    SIZE_MB: '$1 MB',
    SIZE_GB: '$1 GB',
    SIZE_TB: '$1 TB',
    SIZE_PB: '$1 PB',
  };

  window.loadTimeData.getString = id => {
    return window.loadTimeData.data_[id] || id;
  };
}

function testReadEntriesRecursively(callback) {
  let foundEntries = [];
  util.readEntriesRecursively(
      fileSystem.root,
      (entries) => {
        const fileEntries = entries.filter(entry => !entry.isDirectory);
        foundEntries = foundEntries.concat(fileEntries);
      },
      () => {
        // If all directories are read recursively, found files should be 6.
        assertEquals(7, foundEntries.length);
        callback();
      },
      () => {}, () => false);
}

function testReadEntriesRecursivelyLevel0(callback) {
  let foundEntries = [];
  util.readEntriesRecursively(
      fileSystem.root,
      (entries) => {
        const fileEntries = entries.filter(entry => !entry.isDirectory);
        foundEntries = foundEntries.concat(fileEntries);
      },
      () => {
        // If only the top directory is read, found entries should be 3.
        assertEquals(4, foundEntries.length);
        callback();
      },
      () => {}, () => false, 0 /* opt_maxDepth */);
}

function testReadEntriesRecursivelyLevel1(callback) {
  let foundEntries = [];
  util.readEntriesRecursively(
      fileSystem.root,
      (entries) => {
        const fileEntries = entries.filter(entry => !entry.isDirectory);
        foundEntries = foundEntries.concat(fileEntries);
      },
      () => {
        // If we delve directories only one level, found entries should be 5.
        assertEquals(6, foundEntries.length);
        callback();
      },
      () => {}, () => false, 1 /* opt_maxDepth */);
}


function testIsDescendantEntry() {
  const root = fileSystem.root;
  const folder = fileSystem.entries['/dir_a'];
  const subFolder = fileSystem.entries['/dir_a/dir_b'];
  const file = fileSystem.entries['/file_a.txt'];
  const deepFile = fileSystem.entries['/dir_a/dir_b/dir_c/file_g.txt'];

  const fakeEntry =
      new FakeEntry('fake-entry-label', VolumeManagerCommon.RootType.CROSTINI);

  const entryList =
      new EntryList('entry-list-label', VolumeManagerCommon.RootType.MY_FILES);
  entryList.addEntry(fakeEntry);

  const volumeManager = new MockVolumeManager();
  // Index 1 is Downloads.
  assertEquals(
      VolumeManagerCommon.VolumeType.DOWNLOADS,
      volumeManager.volumeInfoList.item(1).volumeType);
  const downloadsVolumeInfo = volumeManager.volumeInfoList.item(1);
  const mockFs = /** @type {MockFileSystem} */ (downloadsVolumeInfo.fileSystem);
  mockFs.populate(['/folder1/']);
  const folder1 = downloadsVolumeInfo.fileSystem.entries['/folder1'];

  const volumeEntry = new VolumeEntry(downloadsVolumeInfo);
  volumeEntry.addEntry(fakeEntry);

  // No descendants.
  assertFalse(util.isDescendantEntry(file, file));
  assertFalse(util.isDescendantEntry(root, root));
  assertFalse(util.isDescendantEntry(deepFile, root));
  assertFalse(util.isDescendantEntry(subFolder, root));
  assertFalse(util.isDescendantEntry(fakeEntry, root));
  assertFalse(util.isDescendantEntry(root, fakeEntry));
  assertFalse(util.isDescendantEntry(fakeEntry, entryList));
  assertFalse(util.isDescendantEntry(fakeEntry, volumeEntry));
  assertFalse(util.isDescendantEntry(folder1, volumeEntry));

  assertTrue(util.isDescendantEntry(root, file));
  assertTrue(util.isDescendantEntry(root, subFolder));
  assertTrue(util.isDescendantEntry(root, deepFile));
  assertTrue(util.isDescendantEntry(root, folder));
  assertTrue(util.isDescendantEntry(folder, subFolder));
  assertTrue(util.isDescendantEntry(folder, deepFile));
  assertTrue(util.isDescendantEntry(entryList, fakeEntry));
  assertTrue(util.isDescendantEntry(volumeEntry, fakeEntry));
  assertTrue(util.isDescendantEntry(volumeEntry, folder1));
}

/**
 * Tests that it doesn't fail with different types of entries and inputs.
 */
function testEntryDebugString() {
  // Check static values.
  assertEquals('entry is null', util.entryDebugString(null));
  (/**
    * @suppress {checkTypes} Closure doesn't allow passing undefined or {} due
    * to type constraints nor casting to {Entry}.
    */
   function() {
     assertEquals('entry is undefined', util.entryDebugString(undefined));
     assertEquals('(Object) ', util.entryDebugString({}));
   })();

  // Construct some types of entries.
  const root = fileSystem.root;
  const folder = fileSystem.entries['/dir_a'];
  const file = fileSystem.entries['/file_a.txt'];
  const fakeEntry =
      new FakeEntry('fake-entry-label', VolumeManagerCommon.RootType.CROSTINI);
  const entryList =
      new EntryList('entry-list-label', VolumeManagerCommon.RootType.MY_FILES);
  entryList.addEntry(fakeEntry);
  const volumeManager = new MockVolumeManager();
  // Index 1 is Downloads.
  assertEquals(
      VolumeManagerCommon.VolumeType.DOWNLOADS,
      volumeManager.volumeInfoList.item(1).volumeType);
  const downloadsVolumeInfo = volumeManager.volumeInfoList.item(1);
  const mockFs = /** @type {MockFileSystem} */ (downloadsVolumeInfo.fileSystem);
  mockFs.populate(['/folder1/']);
  const volumeEntry = new VolumeEntry(downloadsVolumeInfo);
  volumeEntry.addEntry(fakeEntry);

  // Mocked values are identified as Object instead of DirectoryEntry and
  // FileEntry.
  assertEquals(
      '(Object) / filesystem:fake-volume/', util.entryDebugString(root));
  assertEquals(
      '(Object) /dir_a filesystem:fake-volume/dir_a',
      util.entryDebugString(folder));
  assertEquals(
      '(Object) /file_a.txt filesystem:fake-volume/file_a.txt',
      util.entryDebugString(file));
  // FilesAppEntry types:
  assertEquals(
      '(FakeEntry) / fake-entry://crostini', util.entryDebugString(fakeEntry));
  assertEquals(
      '(EntryList) / entry-list://my_files', util.entryDebugString(entryList));
  assertEquals(
      '(VolumeEntry) / filesystem:downloads/',
      util.entryDebugString(volumeEntry));
}

/**
 * Tests the formatting of util.bytesToString
 */
function testBytesToString() {
  const KB = 2 ** 10;
  const MB = 2 ** 20;
  const GB = 2 ** 30;
  const TB = 2 ** 40;
  const PB = 2 ** 50;

  // Up to 1KB is displayed as 'bytes'.
  assertEquals(util.bytesToString(0), '0 bytes');
  assertEquals(util.bytesToString(10), '10 bytes');
  assertEquals(util.bytesToString(KB - 1), '1,023 bytes');
  assertEquals(util.bytesToString(KB), '1 KB');

  // Up to 1MB is displayed as a number of KBs.
  assertEquals(util.bytesToString(2 * KB), '2 KB');
  assertEquals(util.bytesToString(2 * KB + 1), '3 KB');
  assertEquals(util.bytesToString(MB - KB), '1,023 KB');
  assertEquals(util.bytesToString(MB - KB + 1), '1,024 KB');
  assertEquals(util.bytesToString(MB - 1), '1,024 KB');
  assertEquals(util.bytesToString(MB), '1 MB');

  // Up to 1GB is displayed as a number of MBs.
  assertEquals(util.bytesToString(2.55 * MB - 1), '2.5 MB');
  assertEquals(util.bytesToString(2.55 * MB), '2.6 MB');
  assertEquals(util.bytesToString(GB - 0.05 * MB - 1), '1,023.9 MB');
  assertEquals(util.bytesToString(GB - 0.05 * MB), '1,024 MB');
  assertEquals(util.bytesToString(GB - 1), '1,024 MB');
  assertEquals(util.bytesToString(GB), '1 GB');

  // Up to 1TB is displayed as a number of GBs.
  assertEquals(util.bytesToString(2.55 * GB - 1), '2.5 GB');
  assertEquals(util.bytesToString(2.55 * GB), '2.6 GB');
  assertEquals(util.bytesToString(TB - 0.05 * GB - 1), '1,023.9 GB');
  assertEquals(util.bytesToString(TB - 0.05 * GB), '1,024 GB');
  assertEquals(util.bytesToString(TB - 1), '1,024 GB');
  assertEquals(util.bytesToString(TB), '1 TB');

  // Up to 1PB is displayed as a number of GBs.
  assertEquals(util.bytesToString(2.55 * TB - 1), '2.5 TB');
  assertEquals(util.bytesToString(2.55 * TB), '2.6 TB');
  assertEquals(util.bytesToString(PB - 0.05 * TB - 1), '1,023.9 TB');
  assertEquals(util.bytesToString(PB - 0.05 * TB), '1,024 TB');
  assertEquals(util.bytesToString(PB - 1), '1,024 TB');
  assertEquals(util.bytesToString(PB), '1 PB');
}

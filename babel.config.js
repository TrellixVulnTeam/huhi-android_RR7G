module.exports =
{
  "presets": [
    "@babel/preset-react",
    [
      "@babel/preset-env",
      {
        "modules": false,
        "targets": {
          "browsers": [
            "last 1 chrome versions"
          ]
        }
      }
    ]
  ],
  "plugins": [
    "babel-plugin-syntax-dynamic-import",
    "@babel/plugin-proposal-object-rest-spread"
  ]
}

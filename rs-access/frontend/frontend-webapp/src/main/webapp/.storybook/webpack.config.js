// you can use this file to add your custom webpack plugins, loaders and anything you like.
// This is just the basic way to add addional webpack configurations.
// For more information refer the docs: https://goo.gl/qPbSyX

// IMPORTANT
// When you add this file, we won't add the default configurations which is similar
// to "React Create App". This only has babel loader to load JavaScript.

// Webpack configuration file
const CommonConfig = require("../webpack.common.config")
const webpack = require('webpack')
const merge = require('webpack-merge');
const ExtractTextPlugin = require('extract-text-webpack-plugin')

var config = CommonConfig;

config = merge(config, {
  plugins: [
    // your custom plugins
    new webpack.ProvidePlugin({"React": "react",}),
    new ExtractTextPlugin('/css/styles.css', {allChunks: true})
  ],
  devServer: {
    historyApiFallback: {
      // Rewrite to get bundle.js
      rewrites: [{
        from: /\/bundle\.js(\.map)?/,
        to: function (context) {
          return context.match[0]
        }
      }
      ]
    }
  }
});

module.exports = config

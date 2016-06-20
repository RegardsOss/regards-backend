
// Webpack configuration file

const path = require('path')
const webpack = require('webpack')
const autoprefixer = require('autoprefixer')
const ExtractTextPlugin = require('extract-text-webpack-plugin')
const CleanWebpackPlugin = require('clean-webpack-plugin')
const sassLoaders = [
  // Loader to generate react modules css classes
  //'css?modules&importLoaders=1&localIdentName=[path]___[name]__[local]___[hash:base64:5]',
  'css?modules&importLoaders=1&localIdentName=[path]_[name]_[local]',
  // Loader to compile sasss files to css files
  //'sass-loader?indentedSyntax=sass&includePaths[]=' + path.resolve(__dirname, 'steelsheets')
  'sass?sourceMap'
]

module.exports = {
  // Hide stats information from children during webpack compilation
  stats: { children: false },
  // Webpack working directory
  context: __dirname,
  // Javascript main entry
  entry: './scripts/main.js',
  output: {
    // Webpack compilation directory
    path: __dirname + '/build',
    // Webpack main bundle file name
    filename: "bundle.js",
    // Webpack chunks files names
    chunkFilename: "[id].chunck.js"
  },
  devServer: {
    stats: { children: false, colors: true },
    // Web directory serve by the webpack dev server
    contentBase: __dirname,
    // ??? Without this there is no hot replacement during developpment
    inline: true,
    //inline: true,
    port: 3333,
    // Enable rewrite urls for navigation routes generated by the router.
    // Necessary to fallback to root directory when attempt to load
    // webpack generated javascripts.
    historyApiFallback: {
      // Rewrite to get bundle.js
      rewrites: [{
        from: /\/(\d\.)?bundle\.js(\.map)?/,
        to: context => context.match[0]
      },
      // Rewrite to get chunks.
      { from: /\/(\d\.)?chunck\.js(\.map)?/,
        to: context => context.match[0]
      },
      // Rewrite to get styles.css
      { from: /\/(\d\.)?styles\.css(\.map)?/,
        to: context => "/css/"+context.match[0]
      },
      { from: /(\*.jpg)$/,
        to: context => "/img/"+context.match[0]
      },
    ]
    }
  },
  resolve: {
    // Automaticly get extensions files from javascript code with import or require.
    // exemple require('main') look for main, main.js or main.sass with our configuration
    extensions: ['', '.js', '.scss'],
    // Alias that can be used in javascript code for require or import
    // Exemple : require ('AppStore') is equal to require ('scripts/common/store/Store.js');
    alias: {
      RegardsView: path.join(__dirname,"scripts/common/modulesManager/RegardsView.js"),
      AppStore: path.join(__dirname,"scripts/common/store/Store.js"),
      common: path.join(__dirname,"scripts/common")
    },
    // Root directories from wich requires are made
    root: [
      path.join(__dirname,"scripts"),
      path.join(__dirname),
    ]
  },
  module: {
    loaders: [
      // Transpile ES6 Javascript into ES5 with babel loader and react
      {test: /\.js$/, exclude: [/node_modules/,/json/],
        loader: 'babel',
        query: { presets: ['es2015', 'react']}
      },
      // Sass files compilation to css with css modules enable
      {test: /\.scss$/, exclude: [/node_modules/,/scripts/,/stylesheets\/default/,/stylesheets\/vendors/],
        loader: ExtractTextPlugin.extract('style-loader', sassLoaders.join('!'))
      },
      {test: /\.json$/, exclude: [/node_modules/], loader: "json-loader"},
      {test: /\.jpg$/, exclude: [/node_modules/], loader: "file-loader?name=/img/[name].[ext]"},
      {test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/, loader: "url-loader?name=/img/[name].[ext]&limit=10000&minetype=application/font-woff" },
      {test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/, loader: "file-loader?name=/img/[name].[ext]" },
      {test: /\.json$/, loader: "file-loader?name=/json/[name].[ext]"}

    ]
  },
  plugins: [
    // Create a single css file for the whole application
    new ExtractTextPlugin('/css/styles.css',{allChunks: true}),
    new CleanWebpackPlugin(['build'], {
      root: __dirname,
      verbose: false,
      dry: false
    })
  ],
  postcss: [
    // Plugin to Automaticly add vendors prefix to css classes
    autoprefixer({
      browsers: ['last 2 versions']
    })
  ],
  node: {
    net: 'empty',
    tls: 'empty',
    dns: 'empty'
  }
};

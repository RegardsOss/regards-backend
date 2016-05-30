var fs = require('fs')
var path = require('path')
var webpack = require('webpack')
var path = require('path')
const autoprefixer = require('autoprefixer')
const ExtractTextPlugin = require('extract-text-webpack-plugin')

const sassLoaders = [
  'css-loader',
  'postcss-loader',
  'sass-loader?indentedSyntax=sass&includePaths[]=' + path.resolve(__dirname, './src/main/webapp')
]

module.exports = {
  context: __dirname + '/src/main/webapp',
  entry: './main.js',
  output: {
    path: __dirname + '/target/build',
    filename: "bundle.js",
    chunkFilename: "[id].chunck.js"
  },
  devServer: {
    contentBase: __dirname + '/target/build',
    inline: true,
    port: 3333,
    headers: { 'Access-Control-Allow-Origin': '*' },
    historyApiFallback: {
      rewrites: [{
        from: /\/(\d\.)?bundle\.js(\.map)?/,
        to: context => context.match[0]
      },{
        from: /\/(\d\.)?chunck\.js(\.map)?/,
        to: context => context.match[0]
      }]
    }
  },
  resolve: {
    extensions: ['', '.js', '.sass'],
    alias: {
      RegardsView: path.join(__dirname,"src/main/webapp/Common/ModulesManager/RegardsView.js"),
      AppStore: path.join(__dirname,"src/main/webapp/Common/Store/Store.js")
    },
    root: [
      path.join(__dirname,"src/main/webapp"),
    ]
  },
  module: {
    loaders: [
      {test: /\.js$/, exclude: [/node_modules/,/json/],
        loader: 'babel', query: { presets: ['es2015', 'react']}
      },
      {test: /\.css$/, loader: "style-loader!css-loader" },
      {
        test: /\.sass$/,
        loader: ExtractTextPlugin.extract('style-loader', sassLoaders.join('!'))
      },
      {test: /\.(woff|woff2)(\?v=\d+\.\d+\.\d+)?$/, loader: 'url-loader?limit=10000&mimetype=application/font-woff'},
      {test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/, loader: 'url-loader?limit=10000&mimetype=application/octet-stream'},
      {test: /\.eot(\?v=\d+\.\d+\.\d+)?$/, loader: 'file-loader'},
      {test: /\.svg(\?v=\d+\.\d+\.\d+)?$/, loader: 'url-loader?limit=10000&mimetype=image/svg+xml'},
      {test: /\.json(\?v=\d+\.\d+\.\d+)?$/, loader: 'file-loader'}
    ]
  },
  plugins: [
    new ExtractTextPlugin('styles.css')
  ],
  postcss: [
    autoprefixer({
      browsers: ['last 2 versions']
    })
  ]
};

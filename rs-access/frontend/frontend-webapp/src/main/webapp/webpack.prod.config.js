// Webpack configuration file

const path = require('path')
const webpack = require('webpack')
const autoprefixer = require('autoprefixer')
const ExtractTextPlugin = require('extract-text-webpack-plugin')
const CleanWebpackPlugin = require('clean-webpack-plugin')

module.exports = {
  // Hide stats information from children during webpack compilation
  stats: {children: false},
  // Webpack working directory
  context: __dirname,
  // Javascript main entry
  entry: './src/main.tsx',
  output: {
    // Webpack compilation directory
    path: __dirname + '/build',
    // Webpack main bundle file name
    filename: "bundle.js",
    // Webpack chunks files names
    chunkFilename: "[id].chunck.js"
  },
  resolve: {
    // Automaticly get extensions files from javascript code with import or require.
    // exemple require('main') look for main, main.js or main.sass with our configuration
    extensions: ["", ".webpack.js", ".web.js", ".ts", ".tsx", ".js"],
    // Root directories from wich requires are made
    root: [
      path.join(__dirname, "scripts"),
      path.join(__dirname),
    ]
  },
  module: {
    loaders: [
      // All files with a '.ts' or '.tsx' extension will be handled by 'ts-loader'.
      {
        test: /\.tsx{0,1}?$/,
        exclude: [/node_modules/, /json/],
        loader: "babel-loader!ts-loader"
        //loader: "babel-loader?presets=['es2015', 'react']!ts-loader"
      },
      // Transpile ES6 Javascript into ES5 with babel loader and react
      {
        test: /\.js$/,
        exclude: [/node_modules/, /json/],
        loader: 'babel',
        query: {
          presets: ['es2015', 'react'],
        }
      },
      {
        test: /\.css$/,
        loader: ExtractTextPlugin.extract("style-loader", "css-loader")
      },
      {
        test: /\.json$/,
        exclude: [/node_modules/],
        loader: "json-loader"
      },
      {
        test: /\.jpg$/,
        exclude: [/node_modules/],
        loader: "file-loader"
      },
      {
        test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        loader: "url-loader?name=/img/[name].[ext]&limit=10000&minetype=application/font-woff"
      },
      {
        test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        loader: "file-loader?name=/img/[name].[ext]"
      },
      {
        test: /\.json$/,
        loader: "file-loader?name=/json/[name].[ext]"
      }
    ]
  },
  plugins: [
    // Create a single css file for the whole application
    new ExtractTextPlugin('/css/styles.css', {allChunks: true}),
    new webpack.optimize.UglifyJsPlugin({
      // Do not generate source map files (this are usefull during developpment)
      sourceMap: false,
      compress: {
        // Remove warnings generated during compilation
        warnings: false
      }
    }),
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify('production')
      }
    }),
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

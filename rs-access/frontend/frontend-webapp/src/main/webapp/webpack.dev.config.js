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
    chunkFilename: "[id].chunck.js",
    publicPath: "/"
  },
  // Enable sourcemaps for debugging webpack's output.
  devtool: "cheap-module-source-map",
  devServer: {
    stats: {
      children: false,
      colors: true
    },
    // Web directory serve by the webpack dev server
    contentBase: __dirname,
    // ??? Without this there is no hot replacement during developpment
    inline: true,
    //inline: true,
    port: 3333
  },
  resolve: {
    // Automaticly get extensions files from javascript code with import or require.
    // exemple require('main') look for main, main.js or main.sass with our configuration
    // extensions: ['', '.js', '.scss'],
    extensions: ["", ".webpack.js", ".web.js", ".ts", ".tsx", ".js"],
    // Root directories from wich requires are made
    root: [
      path.join(__dirname, "src"),
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
      },
      {test: /\.css$/, loader:  ExtractTextPlugin.extract("style-loader", "css-loader") },
      {test: /\.json$/, exclude: [/node_modules/], loader: "json-loader"},
      {test: /\.jpg$/, exclude: [/node_modules/], loader: "file-loader?name=/img/[name].[ext]"},
      {test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/, loader: "url-loader?name=/img/[name].[ext]&limit=10000&minetype=application/font-woff" },
      {test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/, loader: "file-loader?name=/img/[name].[ext]" },
      {test: /\.json$/, loader: "file-loader?name=/json/[name].[ext]"}
    ],
    preLoaders: [
      // All output '.js' files will have any sourcemaps re-processed by 'source-map-loader'.
      {
        test: /\.js$/,
        loader: "source-map-loader",
        exclude: [/node_modules\/intl-*/]
      }
    ]
  },
  plugins: [
    // Create a single css file for the whole application
    new ExtractTextPlugin('/css/styles.css', {allChunks: true}),
    new CleanWebpackPlugin(['build'], {
      root: __dirname,
      verbose: false,
      dry: false
    }),
    // Allow to define React as a global variable for JSX.
    new webpack.ProvidePlugin({"React": "react",}),
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
  // When importing a module whose path matches one of the following, just
  // assume a corresponding global variable exists and use that instead.
  // This is important because it allows us to avoid bundling all of our
  // dependencies, which allows browsers to cache those libraries between builds.
  // externals: {
  //     "react": "React",
  //     "react-dom": "ReactDOM"
  // },
};

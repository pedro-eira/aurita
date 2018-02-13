var webpack = require('webpack');
var path = require('path');
const CommonsChunkPlugin       = require('webpack/lib/optimize/CommonsChunkPlugin');
const ContextReplacementPlugin = require('webpack/lib/ContextReplacementPlugin');
const CopyWebpackPlugin        = require('copy-webpack-plugin');
const DefinePlugin             = require('webpack/lib/DefinePlugin');

var BUILD_DIR = path.resolve(__dirname, 'javascripts');
var APP_DIR = path.resolve(__dirname, '../app/assets/app');

const ENV  = process.env.NODE_ENV = 'development';
const HOST = process.env.HOST || 'localhost';
const PORT = process.env.PORT || 8080;

const metadata = {
  baseUrl: '/',
  ENV    : ENV,
  host   : HOST,
  port   : PORT
};

var config = {
  devServer: {
    contentBase: '.',
    historyApiFallback: true,
    host: metadata.host,
    port: metadata.port
  },
  devtool: 'source-map',
  entry: {
    main : [
      'webpack-dev-server/client?https://localhost:8080',
      APP_DIR + '/index.js'
    ]
  },
  output: {
    path: BUILD_DIR,
    filename: 'bundle.js',
    sourceMapFilename: "bundle.map",
    publicPath: '/bundles/'
  },
  module : {
    loaders : [
      { test : /\.jsx?/, include : APP_DIR, loader : 'babel-loader' },
      { test: /\.json$/, loader: 'json-loader' },
      { test: /\.css$/,
        loader: 'style-loader!css-loader?modules=true&localIdentName=[name]__[local]___[hash:base64:5]'
      },
      { test: /\.html$/,  loader: 'html-loader?caseSensitive=true' },
      { test: /\.(eot|svg|ttf|woff(2)?)(\?v=\d+\.\d+\.\d+)?/, loader: 'url-loader' }
    ]
  },
  plugins: [
//    new CommonsChunkPlugin({name: 'vendor', filename: 'vendor.bundle.js', minChunks: Infinity}),
    new DefinePlugin({'webpack': {'ENV': JSON.stringify(metadata.ENV)}})
  ],
  resolve: {
    extensions: ['.js', '.jsx'],
    alias: {
      app: APP_DIR
    },
    modules: [
      path.join(__dirname, "node_modules")
    ]
  }
};

module.exports = config;

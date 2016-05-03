/* license_placeholder */
module.exports = function(options) {

    var entry;
    var devPort = "8081";

    // Define entry points according to environment
    if (options.development) {
        entry = {
            // Only one chunk at the moment
            regards: [
                //"webpack-dev-server/client?http://localhost:" + devPort + "/app/",
                //"webpack/hot/only-dev-server",
                "./app/index.ts"
            ]
        };
    } else {
        entry = {
            // Only one chunk at the moment
            regards: "./app/index.ts"
        }
    }

    // Define public path
    var publicPath = options.development ? "http://localhost:" + devPort + "/app/" : "/app/";

    // Export module
    return {
        entry: entry,
        output: {
            path: _ _dirname + "/dist",
            publicPath: publicPath,
            filename: options.development ? "[id].js" : "[name].js"
        },
        resolve: {
            extensions: ["", ".webpack.js", ".web.js", ".ts", ".tsx", ".js"]
        },
        module: {
            loaders: [{
                test: /\.css$/,
                loader: "style!css"
            }, {
                test: /\.tsx?$/,
                exclude: /(node_modules)/,
                loader: "ts-loader"
            }]
        },
        // Instruct webpack to resolve typescript extensions
        resolve: {
            extensions: ["", ".webpack.js", ".web.js", ".js", ".ts", ".tsx"]
        },
        // Webpack dev server
        devServer: {
            colors: true,
            progress: true,
            hot: true,
            port: devPort
        }
    }
};

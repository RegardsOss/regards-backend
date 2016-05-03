module.exports = {

	// Entry base directory
	context : __dirname + "/apps",
	entry : "index.js",

	// Output configuration
	output : {
		path : __dirname + "/dist",
		filename : "bundle.js"
	},
	
	// 
	module : {
		loaders : [ {
			test : /\.css$/,
			loader : "style!css"
		} ]
	}
};
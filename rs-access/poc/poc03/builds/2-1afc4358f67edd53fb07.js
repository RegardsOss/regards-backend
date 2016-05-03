webpackJsonp([2],{

/***/ 12:
/***/ function(module, exports, __webpack_require__) {

	eval("'use strict';\n\nObject.defineProperty(exports, \"__esModule\", {\n    value: true\n});\n\nvar _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();\n\nvar _jquery = __webpack_require__(7);\n\nvar _jquery2 = _interopRequireDefault(_jquery);\n\nvar _mustache = __webpack_require__(9);\n\nvar _mustache2 = _interopRequireDefault(_mustache);\n\nvar _Header = __webpack_require__(13);\n\nvar _Header2 = _interopRequireDefault(_Header);\n\n__webpack_require__(14);\n\nfunction _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }\n\nfunction _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError(\"Cannot call a class as a function\"); } }\n\nvar Header = function () {\n    function Header() {\n        _classCallCheck(this, Header);\n    }\n\n    _createClass(Header, [{\n        key: 'render',\n        value: function render(node) {\n            var text = (0, _jquery2.default)(node).text();\n\n            (0, _jquery2.default)(node).html(_mustache2.default.render(_Header2.default, {\n                text: text\n            }));\n        }\n    }]);\n\n    return Header;\n}();\n\nexports.default = Header;\n\n/*****************\n ** WEBPACK FOOTER\n ** ./src/Components/Header.js\n ** module id = 12\n ** module chunks = 2\n **/\n//# sourceURL=webpack:///./src/Components/Header.js?");

/***/ },

/***/ 13:
/***/ function(module, exports) {

	eval("module.exports = \"<header class=\\\"header\\\">{{text}}</header>\\n\";\n\n/*****************\n ** WEBPACK FOOTER\n ** ./src/Components/Header.html\n ** module id = 13\n ** module chunks = 2\n **/\n//# sourceURL=webpack:///./src/Components/Header.html?");

/***/ },

/***/ 14:
/***/ function(module, exports, __webpack_require__) {

	eval("// style-loader: Adds some css to the DOM by adding a <style> tag\n\n// load the styles\nvar content = __webpack_require__(15);\nif(typeof content === 'string') content = [[module.id, content, '']];\n// add the styles to the DOM\nvar update = __webpack_require__(5)(content, {});\nif(content.locals) module.exports = content.locals;\n// Hot Module Replacement\nif(false) {\n\t// When the styles change, update the <style> tags\n\tif(!content.locals) {\n\t\tmodule.hot.accept(\"!!./../../node_modules/css-loader/index.js!./../../node_modules/sass-loader/index.js!./Header.scss\", function() {\n\t\t\tvar newContent = require(\"!!./../../node_modules/css-loader/index.js!./../../node_modules/sass-loader/index.js!./Header.scss\");\n\t\t\tif(typeof newContent === 'string') newContent = [[module.id, newContent, '']];\n\t\t\tupdate(newContent);\n\t\t});\n\t}\n\t// When the module is disposed, remove the <style> tags\n\tmodule.hot.dispose(function() { update(); });\n}\n\n/*****************\n ** WEBPACK FOOTER\n ** ./src/Components/Header.scss\n ** module id = 14\n ** module chunks = 2\n **/\n//# sourceURL=webpack:///./src/Components/Header.scss?");

/***/ },

/***/ 15:
/***/ function(module, exports, __webpack_require__) {

	eval("exports = module.exports = __webpack_require__(3)();\n// imports\n\n\n// module\nexports.push([module.id, \".header {\\n  font-size: 10rem; }\\n\", \"\"]);\n\n// exports\n\n\n/*****************\n ** WEBPACK FOOTER\n ** ./~/css-loader!./~/sass-loader!./src/Components/Header.scss\n ** module id = 15\n ** module chunks = 2\n **/\n//# sourceURL=webpack:///./src/Components/Header.scss?./~/css-loader!./~/sass-loader");

/***/ }

});
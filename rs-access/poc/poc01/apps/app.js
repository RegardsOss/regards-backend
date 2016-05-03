"use strict";
var greeter = require('./greeter');
var $ = require('jquery');
$(function () {
    $(document.body).html(greeter("World"));
});

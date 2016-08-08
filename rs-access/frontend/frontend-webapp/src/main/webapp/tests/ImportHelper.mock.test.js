/**
 * @source http://stackoverflow.com/questions/32236443/mocha-testing-failed-due-to-css-in-webpack
 * This file is required by mocha when testing.
 * it mocks jpg and others extension imported or required and prevents errors to occurs
 */

const noop = () => 1;

require.extensions['.css'] = noop;
require.extensions['.scss'] = noop;
require.extensions['.png'] = noop;
require.extensions['.jpg'] = noop;
require.extensions['.jpeg'] = noop;
require.extensions['.gif'] = noop;
require.extensions['.svg'] = noop;

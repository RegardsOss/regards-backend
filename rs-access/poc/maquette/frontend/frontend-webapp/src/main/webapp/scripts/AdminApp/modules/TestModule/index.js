import React from 'react';

module.exports = {
  path: 'TestModule',

  getComponents(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./TestModule')
      })
    })
  }
}

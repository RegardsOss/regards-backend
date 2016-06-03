import React from 'react';

module.exports = {
  path: 'test',

  getComponents(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./TestModule')
      })
    })
  }
}

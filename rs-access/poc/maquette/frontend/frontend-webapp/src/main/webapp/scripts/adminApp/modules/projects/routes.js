module.exports = {
  path: 'projects',

  getComponents(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./containers/ProjectsContainer')
      })
    })
  }
}

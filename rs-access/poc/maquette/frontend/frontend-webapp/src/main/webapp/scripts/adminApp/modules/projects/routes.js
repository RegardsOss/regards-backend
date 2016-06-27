import ProjectsContainer from './containers/ProjectsContainer'

export default {
  path: 'projects',
  getComponents(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: ProjectsContainer
      })
    })
  }
}

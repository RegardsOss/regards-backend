import ProjectsContainer from './containers/ProjectsContainer'

declare var require: any;

export default {
  path: 'projects',
  getComponents(nextState: any, cb: any) {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ProjectsContainer
      })
    })
  }
}

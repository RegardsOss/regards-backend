const preloadedState: any = {
  common: {
    theme: 'Light',
    plugins: {},
    api: {
      isFetching: false,
      items: []
    },
    i18n: {
      locale: navigator.language,
      messages: []
    },
    authentication: {},
    endpoints: {
      items: []
    }
  },
  userApp: {
    ws: {
      time: null,
      started: false
    }
  },
  portalApp: {
    projects: {}
  },
  adminApp: {
    projects: {
      items: [
        {
          id: '0',
          name: 'Project X',
          selected: false,
          admins: [0, 1]
        },
        {
          id: '1',
          name: 'Blair witch project',
          selected: false,
          admins: [2, 3]
        }
      ]
    },
    projectAdmins: {
      items: []
    }
  }
}

export default preloadedState

const preloadedState: any = {
  common: {
    theme: 'lightBaseTheme',
    layout: {
      lg: [
        { i: 'sideBar', x: 0, y: 0, w: 2, h: 10 },
        { i: 'appBar', x: 2, y: 0, w: 10, h: 2 },
        { i: 'content', x: 2, y: 0, w: 4, h: 5 },
        { i: 'selectTheme', x: 10, y: 3, w: 2, h: 2 },
      ],
      md: [
        { i: 'sideBar', x: 10, y: 2, w: 2, h: 6 },
        { i: 'appBar', x: 0, y: 0, w: 10, h: 2 },
        { i: 'content', x: 0, y: 0, w: 8, h: 5 },
        { i: 'selectTheme', x: 10, y: 3, w: 2, h: 2 },
      ],
      sm: [
        { i: 'sideBar', x: 0, y: 2, w: 12, h: 2 },
        { i: 'appBar', x: 0, y: 0, w: 10, h: 2 },
        { i: 'content', x: 0, y: 4, w: 12, h: 5 },
        { i: 'selectTheme', x: 0, y: 10, w: 2, h: 2 },
      ]
    },
    plugins: {},
    api: {
      isFetching: false,
      items: []
    },
    i18n: {
      locale: navigator.language,
      messages: []
    },
    authentication: {}
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

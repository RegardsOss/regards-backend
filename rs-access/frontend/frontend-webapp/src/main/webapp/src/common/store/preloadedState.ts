const preloadedState: any = {
  common: {
    theme: 'lightBaseTheme',
    layout: {
      lg: [
        { i: 'appBar', x: 0, y: 0, w: 12, h: 2 },
        { i: 'sideBar', x: 0, y: 2, w: 2, h: 13 },

        { i: 'content', x: 2, y: 2, w: 8, h: 5 },
        { i: 'selectTheme', x: 10, y: 6, w: 2, h: 2 },
        { i: 'selectLanguage', x: 10, y: 2, w: 2, h: 2 },

        { i: 'authentication', x: 4, y: 0, w: 4, h: 8 },
      ],
      md: [
        { i: 'appBar', x: 0, y: 0, w: 12, h: 2 },
        { i: 'sideBar', x: 0, y: 2, w: 4, h: 6 },

        { i: 'content', x: 4, y: 2, w: 8, h: 5 },
        { i: 'selectTheme', x: 0, y: 8, w: 4, h: 2 },
        { i: 'selectLanguage', x: 0, y: 11, w: 4, h: 2 },

        { i: 'authentication', x: 4, y: 0, w: 4, h: 8 },
      ],
      sm: [
        { i: 'appBar', x: 0, y: 0, w: 12, h: 2 },
        { i: 'sideBar', x: 0, y: 2, w: 12, h: 5 },

        { i: 'content', x: 0, y: 7, w: 12, h: 5 },
        { i: 'selectTheme', x: 0, y: 12, w: 4, h: 2 },
        { i: 'selectLanguage', x: 4, y: 12, w: 4, h: 2 },

        { i: 'authentication', x: 2, y: 0, w: 8, h: 8 },
      ],
      xs: [
        { i: 'appBar', x: 0, y: 0, w: 12, h: 2 },
        { i: 'sideBar', x: 0, y: 2, w: 12, h: 6 },

        { i: 'content', x: 0, y: 4, w: 12, h: 5 },
        { i: 'selectTheme', x: 0, y: 10, w: 2, h: 2 },
        { i: 'selectLanguage', x: 10, y: 6, w: 1, h: 2 },

        { i: 'authentication', x: 1, y: 0, w: 10, h: 8 },
      ],
      xxs: [
        { i: 'appBar', x: 0, y: 0, w: 12, h: 2 },
        { i: 'sideBar', x: 0, y: 2, w: 12, h: 6 },

        { i: 'content', x: 0, y: 4, w: 12, h: 5 },
        { i: 'selectTheme', x: 0, y: 10, w: 2, h: 2 },
        { i: 'selectLanguage', x: 10, y: 6, w: 1, h: 2 },

        { i: 'authentication', x: 0, y: 0, w: 12, h: 9 },
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

const preloadedState:any = {
  common: {
    themes: {
      items: ['lightBaseTheme', 'darkBaseTheme'],
      selected: 'lightBaseTheme'
    },
    layout: {
        lg: [
          {i: '1', x: 0, y: 0, w: 1, h: 12},
          {i: '2', x: 1, y: 0, w: 9, h: 12},
          {i: '3', x: 10, y: 0, w: 2, h: 2}
        ],
        md: [
          {i: '1', x: 0, y: 0, w: 1, h: 12},
          {i: '2', x: 1, y: 0, w: 9, h: 12},
          {i: '3', x: 9, y: 0, w: 2, h: 2}
        ],
        sm: [
          {i: '1', x: 0, y: 0, w: 12, h: 2},
          {i: '2', x: 0, y: 1, w: 12, h: 9},
          {i: '3', x: 1, y: 1, w: 2, h: 2}
        ],
        xs: [
          {i: '1', x: 0, y: 0, w: 12, h: 2},
          {i: '2', x: 0, y: 1, w: 12, h: 9},
          {i: '3', x: 1, y: 1, w: 2, h: 2}
        ],
        xxs: [
          {i: '1', x: 0, y: 0, w: 12, h: 2},
          {i: '2', x: 0, y: 1, w: 12, h: 9},
          {i: '3', x: 1, y: 1, w: 2, h: 2}
        ]
      },
    plugins : {},
    views : [],
    authentication : {}
  },
  userApp : {
    ws : {
      time: null,
      started: false
    }
  },
  portalApp : {
    projects : {}
  },
  adminApp : {
    projects : {
      items : [
        {
          id: '0',
          name: 'Project X',
          selected: false,
          admins: [0,1]
        },
        {
          id: '1',
          name: 'Blair witch project',
          selected: false,
          admins: [2,3]
        }
      ]
    },
    // projectAdmins : {
    //   items: [
    //     {
    //       id: '0',
    //       name: 'Toto',
    //       projects: ['0']
    //     },
    //     {
    //       id: '1',
    //       name: 'Titi',
    //       projects: ['1','0']
    //     },
    //     {
    //       id: '2',
    //       name: 'Momo',
    //       projects: ['1']
    //     },
    //     {
    //       id: '3',
    //       name: 'Mimi',
    //       projects: []
    //     }
    //   ]
    // }
  }
}

export default preloadedState

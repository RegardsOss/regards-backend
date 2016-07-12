const preloadedState:any = {
  common: {
    theme: '',
    plugins : {},
    views : [],
    authentication : {}
  },
  userApp : {
    ws : {}
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

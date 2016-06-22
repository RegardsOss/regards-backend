export default {
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
          id: "0",
          name: 'Project X',
          selected: false,
          admins: [
            {
              id: 'toto',
              name: 'Toto'
            },
            {
              id: 'titi',
              name: 'Titi'
            }
          ]
        },
        {
          id: "1",
          name: 'Blair witch project',
          selected: false,
          admins: [
            {
              id: 'momo',
              name: 'Momo'
            },
            {
              id: 'mimi',
              name: 'Mimi'
            }
          ]
        }
      ]
    }
  }
}

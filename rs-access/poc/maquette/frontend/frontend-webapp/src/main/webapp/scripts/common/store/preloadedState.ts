const preloadedState: any = {
    common: {
        themes: {
            items: ['lightBaseTheme', 'darkBaseTheme'],
            selected: 'lightBaseTheme'
        },
        layout: {
            lg: [
                { i: '1', x: 2, y: 0, w: 3, h: 5 },
                { i: '2', x: 3, y: 0, w: 9, h: 2 }
            ],
            md: [
                { i: '1', x: 0, y: 0, w: 3, h: 12 },
                { i: '2', x: 3, y: 0, w: 9, h: 2 }
            ],
            sm: [
                { i: '1', x: 0, y: 0, w: 3, h: 2 },
                { i: '2', x: 10, y: 0, w: 2, h: 2 }
            ],
            xs: [
                { i: '1', x: 0, y: 0, w: 3, h: 2 },
                { i: '2', x: 10, y: 0, w: 2, h: 2 }
            ],
            xxs: [
                { i: '1', x: 0, y: 0, w: 3, h: 2 },
                { i: '2', x: 10, y: 0, w: 2, h: 2 }
            ]
        },
        plugins: {},
        api: {
          isFetching: false,
          items: []
        },
        i18n: {
          locale : navigator.language,
          messages :  []
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

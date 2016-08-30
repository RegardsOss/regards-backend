export default (state: any = {
  isFetching: false,
  items: {},
  ids: [],
  lastUpdate: ''
}, action: any) => {
  switch (action.type) {
    default:
      return state
  }
}


// Selectors
export const getDatasets = (state: any) => state.items

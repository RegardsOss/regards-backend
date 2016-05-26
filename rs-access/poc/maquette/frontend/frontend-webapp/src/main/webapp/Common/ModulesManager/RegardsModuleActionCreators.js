const addViewAccess = (name) => {
  return {
    type : 'ADD_VIEW_ACCESS',
    name : name
  }
}

const viewsLoaded = () => {
  return {
    type : 'VIEWS_LOADED'
  }
}

export { addViewAccess, viewsLoaded  }

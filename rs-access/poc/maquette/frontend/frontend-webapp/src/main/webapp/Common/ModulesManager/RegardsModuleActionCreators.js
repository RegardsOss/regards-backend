const addViewAccess = (name,access) => {
  return {
    type : 'ADD_VIEW_ACCESS',
    name : name,
    access: access
  }
}

export { addViewAccess  }

const setTheme = (theme) => {
  return {
    type : 'SET_THEME',
    theme : theme,
  }
}

const authenticated = () => {
  return {
    type : 'AUTHENTICATED'
  }
}

const logout = () => {
  return {
    type : 'LOGGED_OUT'
  }
}

export { setTheme, authenticated, logout }


const addPlugin = (name, pPlugin) => {
  return {
    type : 'ADD_PLUGIN',
    name : name,
    plugin : pPlugin
  }
}

const pluginsLoaded = () => {
  return {
    type : 'PLUGINS_LOADED'
  }
}

export { addPlugin, pluginsLoaded  }

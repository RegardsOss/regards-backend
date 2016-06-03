
const plugins = (state = [], action) => {
  switch (action.type){
    case "ADD_PLUGIN" :
      return [...state,{
          name : action.name,
          plugin : action.plugin
        }];
    default :
      return state;
  }
}

const pluginsLoaded = (state = false, action) => {
  switch (action.type){
    case "PLUGINS_LOADED" :
      return !state;
    default:
      return state;
  }
}


const pluginReducers = {
  plugins,
  pluginsLoaded
}

export default pluginReducers

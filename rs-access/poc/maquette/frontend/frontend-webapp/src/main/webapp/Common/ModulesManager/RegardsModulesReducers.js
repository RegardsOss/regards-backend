
const views = (state = [], action) => {
  switch (action.type){
    case "ADD_VIEW_ACCESS" :
      return [...state,{
          name : action.name
        }];
    default :
      return state;
  }
}

const viewsLoaded = (state = false, action) => {
  switch (action.type){
    case "VIEWS_LOADED" :
      return !state;
    default:
      return state;
  }
}


const modulesReducers = {
  views,
  viewsLoaded
}

export default modulesReducers

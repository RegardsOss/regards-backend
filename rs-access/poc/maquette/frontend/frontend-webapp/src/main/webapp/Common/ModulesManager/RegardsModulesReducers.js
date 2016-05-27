
const views = (state = [], action) => {
  switch (action.type){
    case "ADD_VIEW_ACCESS" :
      return [...state,{
          name : action.name,
          access: action.access
        }];
    default :
      return state;
  }
}


const modulesReducers = {
  views
}

export default modulesReducers

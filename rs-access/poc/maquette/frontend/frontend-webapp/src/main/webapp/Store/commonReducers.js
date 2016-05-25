

const application = (state = 'UNDEFINED', action) => {
  switch (action.type){
    case "SET_APPLICATION":
      return action.application;
    default:
     return state;
  }
}

const reducers = {
  application
};

export default reducers

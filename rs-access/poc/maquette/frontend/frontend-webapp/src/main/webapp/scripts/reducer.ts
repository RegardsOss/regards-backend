import { combineReducers } from 'redux'
import adminApp from './adminApp/reducer'
import userApp from './userApp/reducers'
import portalApp from './portalApp/reducers'
import common from './common/reducers'
import {reducer as formReducer} from 'redux-form';

export default combineReducers({
  userApp,
  portalApp,
  common,
  adminApp,
  form: formReducer
})

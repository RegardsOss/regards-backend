import { SET_LOCALE, SET_LOCALE_MSG } from "./i18nActions"

interface i18nStore {
  locale: string,
  messages: Object
}

export default (state:i18nStore = {
  locale : navigator.language,
  messages: {}
} , action: any) => {
  switch(action.type){
    // Running fetch plugins from server
    case SET_LOCALE:
      return Object.assign({}, state, {locale: action.locale})
    case SET_LOCALE_MSG:
      return Object.assign({}, state, {messages: action.messages})
    default:
      return state
  }
}

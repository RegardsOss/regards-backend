declare var require: any;

export const SET_LOCALE = 'SET_LOCALE'
export function setLocale(locale:string){
  return {
    type: SET_LOCALE,
    locale: locale
  }
}

export const SET_LOCALE_MSG = 'SET_LOCALE_MSG'
export function setLocaleMessages(messages:Object){
  return {
    type: SET_LOCALE_MSG,
    messages: messages
  }
}

export function updateLocale(locale:string){
  return (dispatch:any, getState:any) => {
    dispatch(setLocale(locale))

    require.ensure([], function(require:any) {
      let messages = require('./messages/messsages.'+locale)
      console.log("messages",messages.default)
      dispatch(setLocaleMessages(messages.default))
    })
  };
}

declare var require: any;
// For tests we need to define require.ensure method if not defined
if (typeof require.ensure !== `function`) require.ensure = (d:any, c:any) => c(require);

import { localeMessagesStore } from './I18nReducers'

export const SET_LOCALE = 'SET_LOCALE'
export function setLocale(locale:string){
  return {
    type: SET_LOCALE,
    locale: locale
  }
}

export const SET_LOCALE_MSG = 'SET_LOCALE_MSG'
export function setLocaleMessages(messagesDir:string, messages:Object){
  return {
    type: SET_LOCALE_MSG,
    messagesDir: messagesDir,
    messages: messages
  }
}

export function updateMessages(messagesDir:string, locale:string){

  return (dispatch:any, getState:any) => {
    require.ensure([], function(require:any) {
      let messages = require('../../'+messagesDir+'/messages.'+locale)
      dispatch(setLocaleMessages(messagesDir, messages.default))
    })
  }
}

export function updateLocale(locale:string){
  return (dispatch:any, getState:any) => {
    return dispatch(setLocale(locale)).then(()=>{
      // Update all messages
      let messages:Array<localeMessagesStore> = getState().common.i18n.messages
      messages.map( (message) => dispatch(updateMessages(message.messagesDir,locale)))
    })

  }
}

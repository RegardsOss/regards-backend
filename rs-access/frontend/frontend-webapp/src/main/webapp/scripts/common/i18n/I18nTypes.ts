
export interface localeMessagesStore {
  messagesDir:string,
  messages:Object
}

export interface i18nStore {
  locale: string,
  messages: Array<localeMessagesStore>
}

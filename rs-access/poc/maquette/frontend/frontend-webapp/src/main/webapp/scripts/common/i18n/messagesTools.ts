declare var require: any;

export function getMessages(language:string, cb: (messages:Object)=>void): void{
  require.ensure([], function(require:any) {
    let messages = require('./messages/messsages.'+language)
    console.log("messages",messages.default)
    cb(messages.default)
  })
}

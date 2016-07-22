/** @module AdminProjectAdmins */
/**
 * This component implements concepts from the
 * AsynchronousBlurValidationForm component and the
 * InitializingFromStateForm component described in the redux-form documentation
 *
 * @see http://redux-form.com/5.2.5/#/examples/asynchronous-blur-validation?_k=2q0jpm
 * @see http://redux-form.com/5.2.5/#/examples/initializing-from-state?_k=7f95k7
 */
import * as React from 'react'
import { Component, PropTypes } from 'react'
import { reduxForm, ReduxFormProps } from 'redux-form'
export const fields = [ 'id', 'projectId', 'username', 'password', 'passwordConfirm']
// var icons = require('../../../../../stylesheets/foundation-icons/foundation-icons.scss')
// Selectors
// import {
//   getSelectedProjectAdminId,
//   getProjectAdminById,
//   getSelectedProjectId } from '../../../reducer'

/**
 * Form fields validation method
 * @param  {[type]} values: any           [description]
 * @return {[type]}         [description]
 */
const validate = (values: any)=> {
  const errors: any = {}
  if (!values.username) {
    errors.username = 'Required'
  }
  if (!values.password) {
    errors.password = 'Required'
  }
  if (!values.passwordConfirm) {
    errors.passwordConfirm = 'Required'
  }
  if (values.password && values.passwordConfirm && values.password !== values.passwordConfirm) {
    errors.passwordConfirm = 'The passwords must match'
  }
  return errors
}

const asyncValidate = (values: any/*, dispatch */) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if ([ 'john', 'paul', 'george', 'ringo' ].find( (value: string) => value === values.username)) {
        reject({ username: 'That username is taken' })
      } else {
        resolve()
      }
    }, 500) // simulate server latency
  })
}

interface FormPropTypes {
  fields?: any,
  resetForm?: any,
  handleSubmit: ()=> void,
  onCancelClick? :()=> void,
  submitting?: boolean,
  show?: boolean,
  onSubmit: () => void
}

/**
 * React component to display a form to add or update an administrator to a given project
 *
 * @prop {Function} handleSubmit Callback to add or update an administrator
 */
class AsynchronousBlurValidationForm extends Component<FormPropTypes, any> {
  render() {
    const {
      asyncValidating,
      fields: { id, projectId, username, password, passwordConfirm },
      resetForm,
      handleSubmit,
      submitting,
      show,
      onCancelClick,
      // styles
    }: any = this.props

    if(show)
      return (
        <form onSubmit={handleSubmit}>
          <div>
            <div>
              <input type="hidden" placeholder="Id" {...id}/>
            </div>
            <div>
              <input type="hidden" placeholder="Project Id" {...projectId}/>
            </div>
            <label>Username</label>
            <div>
              <input type="text" placeholder="Username" {...username} />
              {asyncValidating === 'username' && <i className={null}></i>}
            </div>
            {username.touched && username.error && <div>{username.error}</div>}
          </div>
          <div>
            <label>Password</label>
            <div>
              <input type="password" placeholder="Password" {...password}/>
            </div>
            {password.touched && password.error && <div>{password.error}</div>}
          </div>
          <div>
            <label>Password (confirm)</label>
            <div>
              <input type="password" placeholder="Confirm your password" {...passwordConfirm}/>
            </div>
            {passwordConfirm.touched && passwordConfirm.error && <div>{passwordConfirm.error}</div>}
          </div>
          <div>
            <button
              type="submit"
              // className={styles['button'] + ' ' + styles['success']}
              disabled={submitting}>
              <i className={null}></i>
              {submitting ? 'Submitting...' : ''} Save
            </button>
            <button
              type="button"
              // className={styles['button'] + ' ' + styles['alert']}
              disabled={submitting} onClick={onCancelClick}>
              <i className={null}></i>
              Cancel
            </button>
          </div>
        </form>
      )
    else
      return null
  }
}

// const mapStateToProps = (state: any) => {
//   const selectedProjectAdminId = getSelectedProjectAdminId(state)
//   const selectedProjectAdmin = getProjectAdminById(state, selectedProjectAdminId)
//   const selectedProjectId = getSelectedProjectId(state)
//   return {
//     initialValues: {
//       id: selectedProjectAdminId,
//       username: selectedProjectAdmin ? selectedProjectAdmin.name : '',
//       projectId: selectedProjectId
//     }
//   }
// }

// export default reduxForm({
//   form: 'asynchronousBlurValidation',
//   fields,
//   asyncValidate,
//   asyncBlurFields: [ 'username' ],
//   validate
// }, mapStateToProps)(AsynchronousBlurValidationForm)
export default reduxForm({
  form: 'asynchronousBlurValidation',
  fields,
  asyncValidate,
  asyncBlurFields: [ 'username' ],
  validate
})(AsynchronousBlurValidationForm)

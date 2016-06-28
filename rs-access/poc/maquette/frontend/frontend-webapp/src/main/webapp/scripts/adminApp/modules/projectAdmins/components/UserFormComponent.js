/**
 * This component implements concepts from the
 * AsynchronousBlurValidationForm component and the
 * InitializingFromStateForm component described in the redux-form documentation
 *
 * @see http://redux-form.com/5.2.5/#/examples/asynchronous-blur-validation?_k=2q0jpm
 * @see http://redux-form.com/5.2.5/#/examples/initializing-from-state?_k=7f95k7
 */
import React, { Component, PropTypes } from 'react'
import { reduxForm } from 'redux-form'
export const fields = [ 'id', 'username', 'password', 'passwordConfirm']

const validate = values => {
  const errors = {}
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
    errors.passwordConfirm = 'Must match the password'
  }
  return errors
}

const asyncValidate = (values/*, dispatch */) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if ([ 'john', 'paul', 'george', 'ringo' ].includes(values.username)) {
        reject({ username: 'That username is taken' })
      } else {
        resolve()
      }
    }, 500) // simulate server latency
  })
}

class AsynchronousBlurValidationForm extends Component {
  render() {
    const {
      asyncValidating,
      fields: { id, username, password, passwordConfirm },
      resetForm,
      handleSubmit,
      submitting,
      show,
      onCancelClick
     } = this.props

    if(show)
      return (<form onSubmit={handleSubmit}>
          <div>
            <input type="text" placeholder="Id" {...id}/>
            <label>Username</label>
            <div>
              <input type="text" placeholder="Username" {...username} />
              {asyncValidating === 'username' && <i /* spinning cog *//>}
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
            <button type="submit" disabled={submitting}>
              {submitting ? <i/> : <i/>} Sign Up
            </button>
            <button type="button" disabled={submitting} onClick={onCancelClick}>
              Cancel
            </button>
          </div>
        </form>
      )
    else
      return null
  }
}

AsynchronousBlurValidationForm.propTypes = {
  fields: PropTypes.object.isRequired,
  resetForm: PropTypes.func.isRequired,
  handleSubmit: PropTypes.func.isRequired,
  submitting: PropTypes.bool.isRequired
}


export default reduxForm({
  form: 'asynchronousBlurValidation',
  fields,
  asyncValidate,
  asyncBlurFields: [ 'username' ],
  validate
},
state => ({
  username: 'titi',
  password: 'salut'
}))(AsynchronousBlurValidationForm)

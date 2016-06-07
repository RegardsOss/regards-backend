import {expect} from 'chai';
import React from 'react'
import { shallow } from 'enzyme';
import LoginComponent from '../../../scripts/adminApp/components/LoginComponent';

// Test a component rendering

describe('Testing login component', () => {
  it('Should render correctly the login component', () => {
    const onLogin = (username, password) => { };
    const loginStyles = {
      "login-modal": 'login-modal',
      "login-error": "login-error"
    };
    let props = {
      onLogin: onLogin,
      errorMessage: '',
      styles: loginStyles
    };

    const wrapper = shallow(<LoginComponent {...props}/>);
    expect(wrapper.find("div .login-modal")).to.have.length(1);
    expect(wrapper.find("div .login-error")).to.have.length(1);
    expect(wrapper.find("input #username")).to.have.length(1);
    const username = wrapper.find("input #username");
    username.simulate('change', { target: { value: 'test' } });
    expect(wrapper.find("input #password")).to.have.length(1);
    const password = wrapper.find("input #password");
    password.simulate('change', { target: { value: 'test_password' } });
    expect(wrapper.find("button")).to.have.length(1);

    // Test onLogin action
    wrapper.find("button").simulate('click');
  });

  it('Should active login action correctly', () => {
    const onLogin = (username, password) => {
      expect(username).to.equal("test");
      expect(password).to.equal("test_password");
    };
    const handleKeyPress = () => { };
    const loginStyles = {
      "login-modal": 'login-modal',
      "login-error": "login-error"
    };
    let props = {
      onLogin: onLogin,
      errorMessage: '',
      styles: loginStyles
    };

    const wrapper = shallow(<LoginComponent {...props}/>);
    const username = wrapper.find("input #username");
    username.simulate('change', { target: { value: 'test' } });
    const password = wrapper.find("input #password");
    password.simulate('change', { target: { value: 'test_password' } });
    // Test onLogin action
    wrapper.find("button").simulate('click');
  });

});

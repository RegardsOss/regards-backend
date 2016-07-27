import * as React from 'react'
import { shallow, mount } from 'enzyme';
import { expect } from 'chai';
import { forEach } from 'lodash'
import { SelectTheme } from '../../../../scripts/common/theme/containers/SelectTheme'
import ThemeHelper from '../../../../scripts/common/theme/ThemeHelper'
import {Card, CardText} from 'material-ui/Card'
import SelectField from 'material-ui/SelectField'
import MenuItem from 'material-ui/MenuItem'

function setup() {
  const props = {
    theme: 'titi'
  }
  const enzymeWrapper = shallow(<SelectTheme {...props}/>)
  return {
    props,
    enzymeWrapper
  }
}

// Test a component rendering
describe('[COMMON] Testing select theme container', () => {

  /**
   * Not tested
   * Behaviour is expected to be extracted from mapStateToProps
   * to be moved to selectors
   *
   * @see http://randycoulman.com/blog/2016/03/15/testing-redux-applications/
   */
  // it('should get state mapped to props', () => {
  // })

  /**
   * Not tested
   * Trivial and not worth testing
   *
   * @see http://randycoulman.com/blog/2016/03/15/testing-redux-applications/
   */
  // it('should get dispatch mapped to props', () => {
  //
  // });

  it('should render self and subcomponents', () => {
    // Mock the themes list
    ThemeHelper.getThemes = () => ['titi', 'toto']

    const { enzymeWrapper } = setup()
    // const expectedDOM =  (
    //   <Card>
    //     <CardText>
    //       <SelectField
    //         value='titi'
    //         onChange={this.handleChange} ??
    //         fullWidth={true} >
    //         <MenuItem value='titi' key='titi' primaryText='titi' />
    //         <MenuItem value='toto' key='toto' primaryText='toto' />
    //       </SelectField>
    //     </CardText>
    //   </Card>
    // )
    const card = enzymeWrapper.find(Card)
    expect(card).to.have.length(1)
    const cardText = card.find(CardText)
    expect(cardText).to.have.length(1)
    const selectField = cardText.find(SelectField)
    expect(selectField).to.have.length(1)
    const selectFieldProps = selectField.props()
    expect(selectFieldProps.value).to.equal('titi')
    expect(selectFieldProps.fullWidth).to.equal(true)
    // expect(selectFieldProps.onChange).to.equal(SelectTheme.prototype.handleChange)
    const menuItems = selectField.find(MenuItem)
    expect(menuItems).to.have.length(2)
    // expect(menuItem0Props.value).to.equal('titi')
    // expect(menuItem0Props.key).to.equal('titi')
    // expect(menuItem0Props.primaryText).to.equal('titi')
    // const menuItem1Props: MenuItemPropTypes = menuItems[1].props()
    // expect(menuItem1Props.value).to.equal('toto')
    // expect(menuItem1Props.key).to.equal('toto')
    // expect(menuItem1Props.primaryText).to.equal('toto')
  })

});

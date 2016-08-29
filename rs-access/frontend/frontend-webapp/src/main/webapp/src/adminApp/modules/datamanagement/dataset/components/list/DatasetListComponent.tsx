import * as React from "react"
import { Card, CardTitle, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import CancelButtonComponent from "../../../components/CancelButtonComponent"
import MainButtonComponent from "../../../components/MainButtonComponent"
import { TableRowColumn, Table, TableBody, TableHeader, TableHeaderColumn, TableRow } from "material-ui/Table"
import { Dataset } from "../../Dataset"
import Delete from "material-ui/svg-icons/action/delete"
import { map } from "lodash"
import FlatButton from "material-ui/FlatButton"
import Edit from "material-ui/svg-icons/editor/mode-edit"


interface DatasetListProps {
  getBackUrl: () => string
  getCreateUrl: () => string
  datasets: Array<Dataset>
}
/**
 */
export default class DatasetListComponent extends React.Component<DatasetListProps, any> {


  getCreateUrl = (): string => {
    return this.props.getCreateUrl()
  }
  getBackUrl = (): string => {
    return this.props.getBackUrl()
  }


  render (): JSX.Element {
    const {datasets} = this.props
    const styleCardActions = {
      display: "flex",
      flexDirection: "row",
      justifyContent: "flex-end"
    }
    return (
      <Card
        initiallyExpanded={true}>
        <CardTitle
          title={<FormattedMessage id="datamanagement.dataset.list.header"/>}
        />
        <CardText>
          <Table
            selectable={false}
            multiSelectable={false}
          >
            <TableHeader
              enableSelectAll={false}
              adjustForCheckbox={false}
              displaySelectAll={false}
            >
              <TableRow>
                <TableHeaderColumn>
                  <FormattedMessage
                    id="datamanagement.dataset.table.name"
                  />
                </TableHeaderColumn>
                <TableHeaderColumn>
                  <FormattedMessage
                    id="datamanagement.dataset.table.actions"
                  />
                </TableHeaderColumn>
              </TableRow>
            </TableHeader>
            <TableBody displayRowCheckbox={false} preScanRows={false}>
              {map(datasets, (dataset: Dataset, id: number) => (

                <TableRow
                  key={id}>
                  <TableRowColumn>
                    {dataset.name}
                  </TableRowColumn>
                  <TableRowColumn>
                    <FlatButton icon={<Delete />} disabled={true}/>
                    <FlatButton icon={<Edit />} disabled={true}/>
                  </TableRowColumn>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div style={styleCardActions}>
            <CancelButtonComponent
              label={<FormattedMessage
                    id="datamanagement.dataset.list.action.cancel"
                  />}
              url={this.getBackUrl()}
            />
            <MainButtonComponent
              label={<FormattedMessage
                    id="datamanagement.dataset.list.action.add"
                  />}
              url={this.getCreateUrl()}
            />
          </div>
        </CardText>
      </Card>
    )
  }
}

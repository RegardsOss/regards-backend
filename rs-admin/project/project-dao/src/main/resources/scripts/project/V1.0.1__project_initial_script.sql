alter table t_project ALTER COLUMN description SET DATA TYPE text;
/* column label is not null  so lets give it a default value which is not stupid in 3 steps
    1. add the column without constraint
    2. set the default to project name which is also not null
    3. add the constraint
*/
alter table t_project add column label varchar(256);
update t_project set label=name where name is not null;
alter table t_project alter column label set not null;
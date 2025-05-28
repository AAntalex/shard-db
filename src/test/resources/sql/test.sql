select p.ID, p.c_num, acc_dt.C_CODE, acc_Ct.C_CODE, cl_dt.c_name, cl_ct.c_name, cat.c_code
from t_payment p
         right join T_ACCOUNT acc_ct on acc_ct.ID = p.c_acc_ct
         right join T_ACCOUNT acc_dt on acc_dt.ID = p.c_acc_dt
         left join t_client cl_dt on acc_dt.C_CLIENT = cl_dt.ID
         left join t_client cl_ct on acc_ct.C_CLIENT = cl_ct.ID
         left join t_client_category cat on cat.ID = cl_ct.c_category
         left join t_client_category cat_dt on cat_dt.ID = cl_dt.c_category
where acc_ct.c_code = '40702810X00000001001'


select p.ID, p.c_num, acc_dt.C_CODE, acc_Ct.C_CODE, cl_dt.c_name, cl_ct.c_name, cat.c_code
from T_ACCOUNT acc_ct
         right join t_payment p on acc_ct.ID = p.c_acc_ct
         left join T_ACCOUNT acc_dt on acc_dt.ID = p.c_acc_dt
         left join t_client cl_dt on acc_dt.C_CLIENT = cl_dt.ID
         left join t_client cl_ct on acc_ct.C_CLIENT = cl_ct.ID
         right join t_client_category cat on cat.ID = cl_ct.c_category

select * from t_client

select acc_dt.C_CODE, cl.c_name, cat.c_code,  p.c_num
from T_ACCOUNT acc_dt
         left join t_client cl on acc_dt.C_CLIENT = cl.ID
         join t_client_category cat on cat.ID = cl.c_category
         right join t_payment p on acc_dt.ID = p.c_acc_dt
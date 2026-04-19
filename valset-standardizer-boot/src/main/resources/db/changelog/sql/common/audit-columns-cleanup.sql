--liquibase formatted sql


--changeset codex:20260416-02-oracle-drop-audit-columns dbms:oracle
DECLARE
    v_count NUMBER := 0;
BEGIN
    SELECT COUNT(1) INTO v_count FROM user_tab_columns WHERE table_name = 'TR_FM_JJHZGZB' AND column_name = 'IS_AUDT';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TR_FM_JJHZGZB DROP COLUMN IS_AUDT';
    END IF;

    SELECT COUNT(1) INTO v_count FROM user_tab_columns WHERE table_name = 'TR_FM_JJHZGZB' AND column_name = 'AUDT_ID';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TR_FM_JJHZGZB DROP COLUMN AUDT_ID';
    END IF;

    SELECT COUNT(1) INTO v_count FROM user_tab_columns WHERE table_name = 'T_TR_JJHZGZB' AND column_name = 'IS_AUDT';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE T_TR_JJHZGZB DROP COLUMN IS_AUDT';
    END IF;

    SELECT COUNT(1) INTO v_count FROM user_tab_columns WHERE table_name = 'T_TR_JJHZGZB' AND column_name = 'AUDT_ID';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE T_TR_JJHZGZB DROP COLUMN AUDT_ID';
    END IF;

    SELECT COUNT(1) INTO v_count FROM user_tab_columns WHERE table_name = 'T_TR_INDEX' AND column_name = 'IS_AUDT';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE T_TR_INDEX DROP COLUMN IS_AUDT';
    END IF;

    SELECT COUNT(1) INTO v_count FROM user_tab_columns WHERE table_name = 'T_TR_INDEX' AND column_name = 'AUDT_ID';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE T_TR_INDEX DROP COLUMN AUDT_ID';
    END IF;
END;
/

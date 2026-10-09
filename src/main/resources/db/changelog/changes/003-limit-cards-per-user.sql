--liquibase formatted sql

--changeset alexp:003-limit-cards-per-user splitStatements:false
CREATE FUNCTION check_cards_limit() RETURNS trigger AS $$
BEGIN
    PERFORM 1 FROM users WHERE id = NEW.user_id FOR UPDATE;
    IF (SELECT count(*) FROM payment_cards WHERE user_id = NEW.user_id AND id <> NEW.id) >= 5 THEN
        RAISE EXCEPTION 'User % already has the maximum of 5 cards', NEW.user_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_cards_limit
    BEFORE INSERT OR UPDATE OF user_id ON payment_cards
    FOR EACH ROW EXECUTE FUNCTION check_cards_limit();
--rollback DROP TRIGGER trg_payment_cards_limit ON payment_cards;
--rollback DROP FUNCTION check_cards_limit();

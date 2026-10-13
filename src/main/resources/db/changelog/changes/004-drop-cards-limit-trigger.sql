--liquibase formatted sql

--changeset alexp:004-drop-cards-limit-trigger splitStatements:false
DROP TRIGGER IF EXISTS trg_payment_cards_limit ON payment_cards;
DROP FUNCTION IF EXISTS check_cards_limit();
--rollback CREATE FUNCTION check_cards_limit() RETURNS trigger AS $$
--rollback BEGIN
--rollback     PERFORM 1 FROM users WHERE id = NEW.user_id FOR UPDATE;
--rollback     IF (SELECT count(*) FROM payment_cards WHERE user_id = NEW.user_id AND id <> NEW.id) >= 5 THEN
--rollback         RAISE EXCEPTION 'User % already has the maximum of 5 cards', NEW.user_id
--rollback             USING ERRCODE = 'check_violation';
--rollback     END IF;
--rollback     RETURN NEW;
--rollback END;
--rollback $$ LANGUAGE plpgsql;
--rollback CREATE TRIGGER trg_payment_cards_limit
--rollback     BEFORE INSERT OR UPDATE OF user_id ON payment_cards
--rollback     FOR EACH ROW EXECUTE FUNCTION check_cards_limit();

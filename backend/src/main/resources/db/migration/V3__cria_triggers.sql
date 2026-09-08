CREATE FUNCTION validar_pagina_maxima()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.pagina_maxima < OLD.pagina_maxima THEN
        RAISE EXCEPTION 'A página máxima não pode diminuir';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validar_pagina_maxima_antes_de_atualizar
BEFORE UPDATE OF pagina_maxima
ON item_biblioteca
FOR EACH ROW
EXECUTE FUNCTION validar_pagina_maxima();
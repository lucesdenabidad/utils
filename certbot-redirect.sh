#!/bin/bash

# Solicitar inputs al usuario
echo "Por favor ingresa el dominio (ejemplo: midominio.com):"
read domain

echo "Por favor ingresa la IP o URL a la que deseas redirigir (ejemplo: 192.168.1.1 o app.ejemplo.com):"
read target

# Verificar que los inputs no estén vacíos
if [ -z "$domain" ] || [ -z "$target" ]; then
    echo "Error: El dominio y la IP/URL son requeridos"
    exit 1
fi

# Instalar certbot si no está instalado
if ! command -v certbot &> /dev/null; then
    echo "Instalando certbot..."
    apt-get update
    apt-get install -y certbot python3-certbot-nginx
fi

# Crear configuración de nginx
config_file="/etc/nginx/sites-available/$domain"
cat > "$config_file" << EOF
server {
    listen 80;
    listen [::]:80;
    server_name $domain;

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name $domain;

    ssl_certificate /etc/letsencrypt/live/$domain/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$domain/privkey.pem;

    location / {
        proxy_pass http://$target;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# Crear enlace simbólico
ln -sf "$config_file" /etc/nginx/sites-enabled/

# Obtener certificado SSL con certbot
certbot --nginx -d "$domain" --non-interactive --agree-tos --email admin@"$domain" --redirect

# Reiniciar nginx
systemctl restart nginx

echo "¡Configuración completada!"
echo "Dominio: $domain"
echo "Redirigiendo a: $target"
